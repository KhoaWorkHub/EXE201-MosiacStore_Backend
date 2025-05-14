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
RUNNING_CONTAINER=$(docker ps --format "{{.Names}}" | grep -E "vietshirt-app(-green)?$" || echo "")

if [ -n "$RUNNING_CONTAINER" ]; then
  # Container đang chạy, lấy port
  CURRENT_PORT=$(docker ps --filter "name=$RUNNING_CONTAINER" --format "{{.Ports}}" | grep -o "0.0.0.0:[0-9]*->8080" | cut -d ":" -f2 | cut -d "-" -f1)

  # Xác định container name
  if [ "$RUNNING_CONTAINER" = "vietshirt-app" ]; then
    CURRENT_CONTAINER=$BLUE_CONTAINER
    NEW_CONTAINER=$GREEN_CONTAINER
    NEW_PORT=$GREEN_PORT
  else
    CURRENT_CONTAINER=$GREEN_CONTAINER
    NEW_CONTAINER=$BLUE_CONTAINER
    NEW_PORT=$BLUE_PORT
  fi
else
  # Không có container nào đang chạy
  CURRENT_CONTAINER=""
  CURRENT_PORT=""
  NEW_CONTAINER=$BLUE_CONTAINER
  NEW_PORT=$BLUE_PORT
fi

# Đảm bảo CURRENT_PORT luôn có giá trị
if [ -z "$CURRENT_PORT" ]; then
  if [ "$NEW_PORT" = "8080" ]; then
    CURRENT_PORT=8081
  else
    CURRENT_PORT=8080
  fi
  log "CURRENT_PORT không xác định, sử dụng giá trị mặc định: $CURRENT_PORT"
fi

# Tạo file .env mới cho container mới
cat > .env.new << EOL
CONTAINER_NAME=$NEW_CONTAINER
HOST_PORT=$NEW_PORT

# Các biến môi trường từ file .env gốc
$(grep -v "CONTAINER_NAME\|HOST_PORT" .env)
EOL

# Pull image mới nhất
log "Pulling latest image..."
docker compose pull

# Khởi động container mới
log "Starting new container $NEW_CONTAINER on port $NEW_PORT..."
HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker compose --env-file .env.new up -d

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
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker compose --env-file .env.new down
  exit 1
fi

# Cập nhật cấu hình Nginx
log "Cập nhật cấu hình Nginx để chuyển lưu lượng..."
ls -la $NGINX_CONF 2>&1 || log "Không thể truy cập file cấu hình: $NGINX_CONF"

# Đảm bảo CURRENT_PORT luôn có giá trị
if [ -z "$CURRENT_PORT" ]; then
  # Nếu CURRENT_PORT trống, gán giá trị mặc định
  if [ "$NEW_PORT" = "8080" ]; then
    CURRENT_PORT=8081
  else
    CURRENT_PORT=8080
  fi
  log "CURRENT_PORT không xác định, sử dụng giá trị mặc định: $CURRENT_PORT"
fi

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

# Sao lưu vào thư mục người dùng (không cần sudo)
mkdir -p ~/nginx-backups
cat $NGINX_CONF > ~/nginx-backups/vietshirt.backup.$(date +%Y%m%d%H%M%S) 2>/dev/null || true

# Ghi nội dung cấu hình vào file
log "Đang cập nhật file cấu hình..."
if ! echo "$NGINX_CONFIG" | sudo /bin/tee $NGINX_CONF > /dev/null 2>&1; then
  log "LỖI: Không thể ghi vào file cấu hình Nginx. Chi tiết:"
  echo "$NGINX_CONFIG" | sudo /bin/tee $NGINX_CONF 2>&1 || log "Không thể hiển thị lỗi cụ thể"
  log "Hủy triển khai..."
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker compose --env-file .env.new down
  exit 1
fi

# Kiểm tra và reload Nginx
log "Kiểm tra cấu hình Nginx..."
if sudo /usr/sbin/nginx -t; then
  log "Cấu hình Nginx hợp lệ, áp dụng thay đổi..."
  sudo /bin/systemctl reload nginx
else
  log "LỖI: Cấu hình Nginx không hợp lệ. Đang hủy triển khai..."
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker compose --env-file .env.new down
  exit 1
fi

# Cập nhật file .env chính
mv .env.new .env

# Đợi một khoảng thời gian để đảm bảo các kết nối hiện tại hoàn tất
log "Đợi để các kết nối hiện tại kết thúc (30 giây)..."
sleep 30

# Dừng container cũ
log "Dừng container cũ ($CURRENT_CONTAINER)..."
HOST_PORT=$CURRENT_PORT CONTAINER_NAME=$CURRENT_CONTAINER docker compose down

log "Triển khai hoàn tất thành công! Ứng dụng đang chạy trên container $NEW_CONTAINER, port $NEW_PORT"

if ! docker ps | grep -q "$NEW_CONTAINER"; then
  log "Phát hiện container không chạy sau khi triển khai, thử khởi động lại..."
  cd $APP_DIR
  HOST_PORT=$NEW_PORT CONTAINER_NAME=$NEW_CONTAINER docker compose --env-file .env up -d
fi