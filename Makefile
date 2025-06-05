# Makefile
.PHONY: start stop reset status mysql

start:
	@echo "🚀 Ocean DB 시작 중..."
	@cd docker && docker-compose up -d
	@echo "✅ Ocean DB 시작 완료!"
	@echo "📊 phpMyAdmin: http://localhost:8081"

stop:
	@echo "🛑 Ocean DB 중지 중..."
	@cd docker && docker-compose stop
	@echo "✅ Ocean DB 중지 완료!"

reset:
	@echo "🔄 Ocean DB 초기화 중..."
	@cd docker && docker-compose down -v
	@cd docker && docker-compose up -d
	@echo "✅ Ocean DB 초기화 완료!"

status:
	@echo "📊 Ocean 컨테이너 상태:"
	@docker ps --filter "name=ocean"

mysql:
	@echo "🔗 MySQL 접속 중..."
	@docker exec -it ocean-mysql mysql -uocean_user -pocean_pass ocean_db

logs:
	@cd docker && docker-compose logs -f