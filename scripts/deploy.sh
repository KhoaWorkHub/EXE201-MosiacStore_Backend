#!/bin/bash
set -e

# Định nghĩa biến
APP_DIR=~/vietshirt-app
BLUE_PORT=8080
GREEN_PORT=8081
BLUE_CONTAINER="vietshirt-app"
GREEN_CONTAINER="vietshirt-app-green"
HEALTH_CHECK_PATH="/actuator/health"
MAX_RETRIES=30
RETRY_INTERVAL=2
NGINX_CONF="/etc/nginx/sites-available/vietshirt"

# Hàm kiểm tra health
check_health() {
  local port=$1
  local url="http://localhost:${port}${HEALTH_CHECK_PATH}"
  local status=$(curl -s -o /dev/null -w "%{http_code}" $url || echo "000")
  echo $status
}

# Log function
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Bắt đầu quá trình triển khai zero-downtime..."

# Xác định trạng thái hiện tại
cd $APP_DIR
CURRENT_PORT=$(docker ps --filter "name=vietshirt-app" --format "{{.Ports}}" | grep -o "0.0.0.0:[0-9]*->8080" | cut -d ":" -f2 | cut -d "-" -f1 || echo $BLUE_PORT)

# Xác định cấu hình mới
if [ "$CURRENT_PORT" = "$BLUE_PORT" ]; then
  NEW_PORT=$GREEN_PORT
  NEW_CONTAINER=$GREEN_CONTAINER
  CURRENT_CONTAINER=$BLUE_CONTAINER
else
  NEW_PORT=$BLUE_PORT
  NEW_CONTAINER=$BLUE_CONTAINER
  CURRENT_CONTAINER=$GREEN_CONTAINER
fi

log "Trạng thái hiện tại: Container $CURRENT_CONTAINER đang chạy trên port $CURRENT_PORT"
log "Triển khai mới: Container $NEW_CONTAINER sẽ chạy trên port $NEW_PORT"

# Tạo file .env mới cho container mới
cat > .env.new << EOL
CONTAINER_NAME=$NEW_CONTAINER
HOST_PORT=$NEW_PORT

# Các biến môi trường từ file .env gốc
$(grep -v "CONTAINER_NAME\|HOST_PORT" .env)
EOL

# Pull image mới nhất
log "Pulling latest image..."
docker-compose pull

# Khởi động container mới
log "Starting new container $NEW_CONTAINER on port $NEW_PORT..."
HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker-compose --env-file .env.new up -d

# Kiểm tra health của container mới
log "Kiểm tra sức khỏe của container mới..."
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  HEALTH_STATUS=$(check_health $NEW_PORT)

  if [ "$HEALTH_STATUS" = "200" ] || [ "$HEALTH_STATUS" = "302" ]; then
    log "Container mới hoạt động tốt! (Status: $HEALTH_STATUS)"
    break
  fi

  RETRY_COUNT=$((RETRY_COUNT + 1))
  log "Kiểm tra lần $RETRY_COUNT/$MAX_RETRIES: Status $HEALTH_STATUS - đang chờ..."
  sleep $RETRY_INTERVAL
done

# Kiểm tra kết quả health check
if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
  log "LỖI: Container mới không vượt qua kiểm tra sức khỏe. Đang hủy triển khai..."
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker-compose --env-file .env.new down
  exit 1
fi

# Cập nhật cấu hình Nginx
log "Cập nhật cấu hình Nginx để chuyển lưu lượng..."

NGINX_CONFIG="# Định nghĩa upstream backend
upstream vietshirt_backend {
    server localhost:$NEW_PORT;
    server localhost:$CURRENT_PORT backup;
}

server {
    listen 80;
    server_name 54.169.96.88;

    location / {
        # Sử dụng upstream thay vì hardcoded localhost:8080
        proxy_pass http://vietshirt_backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # Sử dụng biến để xác định nguồn origin
        set \$cors_origin \"\";
        if (\$http_origin ~ \"^(https://mosiacstore\\.vercel\\.app|http://localhost:5173)\$\") {
            set \$cors_origin \$http_origin;
        }

        # CORS headers với origin động
        add_header 'Access-Control-Allow-Origin' \$cors_origin always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS, PUT, DELETE, PATCH' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Expose-Headers' 'Content-Length,Content-Range' always;

        # Preflight requests
        if (\$request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' \$cors_origin always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS, PUT, DELETE, PATCH' always;
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
            add_header 'Access-Control-Max-Age' 1728000 always;
            add_header 'Content-Type' 'text/plain charset=UTF-8' always;
            add_header 'Content-Length' 0 always;
            return 204;
        }
    }

    # Endpoint riêng cho health check
    location /actuator/health {
        proxy_pass http://vietshirt_backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}"

# Ghi nội dung cấu hình vào file
echo "$NGINX_CONFIG" | sudo tee $NGINX_CONF > /dev/null

# Kiểm tra và reload Nginx
log "Kiểm tra cấu hình Nginx..."
sudo nginx -t

if [ $? -eq 0 ]; then
  log "Cấu hình Nginx hợp lệ, áp dụng thay đổi..."
  sudo systemctl reload nginx
else
  log "LỖI: Cấu hình Nginx không hợp lệ. Đang hủy triển khai..."
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker-compose --env-file .env.new down
  exit 1
fi

# Cập nhật file .env chính
mv .env.new .env

# Đợi một khoảng thời gian để đảm bảo các kết nối hiện tại hoàn tất
log "Đợi để các kết nối hiện tại kết thúc (30 giây)..."
sleep 30

# Dừng container cũ
log "Dừng container cũ ($CURRENT_CONTAINER)..."
HOST_PORT=$CURRENT_PORT CONTAINER_NAME=$CURRENT_CONTAINER docker-compose down

log "Triển khai hoàn tất thành công! Ứng dụng đang chạy trên container $NEW_CONTAINER, port $NEW_PORT"