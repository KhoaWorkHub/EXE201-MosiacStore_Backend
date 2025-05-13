#!/bin/bash
set -e

# Cập nhật docker-compose.yml nếu cần
cd ~/vietshirt-app

if ! grep -q 'container_name: \${CONTAINER_NAME' docker-compose.yml; then
  echo 'Cập nhật docker-compose.yml để hỗ trợ biến môi trường...'
  cp docker-compose.yml docker-compose.yml.backup
  sed -i 's/container_name: vietshirt-app/container_name: \${CONTAINER_NAME:-vietshirt-app}/' docker-compose.yml
  sed -i 's/- \"8080:8080\"/- \"\${HOST_PORT:-8080}:8080\"/' docker-compose.yml
fi

# Tạo file .env nếu chưa có
if [ ! -f .env ]; then
  echo 'Tạo file .env ban đầu...'
  cat > .env << 'EOL'
CONTAINER_NAME=vietshirt-app
HOST_PORT=8080
# Nội dung này cần được cập nhật với các biến môi trường thực tế của bạn
EOL
fi

# Thiết lập quyền sudo cho Nginx
if ! sudo grep -q 'ubuntu.*NOPASSWD.*nginx' /etc/sudoers; then
  echo 'Cấu hình quyền sudo cho Nginx...'
  echo 'ubuntu ALL=(ALL) NOPASSWD: /usr/sbin/nginx, /bin/systemctl reload nginx, /usr/bin/tee /etc/nginx/sites-available/vietshirt' | sudo tee -a /etc/sudoers
fi

# Cấu hình Spring Boot Actuator
if [ -f "src/main/resources/application.properties" ] && ! grep -q "management.endpoints.web.exposure.include" src/main/resources/application.properties; then
  echo "Cấu hình Spring Boot Actuator..."
  echo "" >> src/main/resources/application.properties
  echo "# Actuator Configuration" >> src/main/resources/application.properties
  echo "management.endpoints.web.exposure.include=health" >> src/main/resources/application.properties
  echo "management.endpoint.health.show-details=always" >> src/main/resources/application.properties
fi

echo "Thiết lập điều kiện tiên quyết hoàn tất."