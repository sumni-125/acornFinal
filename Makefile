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

update-db:
	@echo "📊 DB 스키마 업데이트 중..."
	@if [ -f docker/mysql/updates/latest.sql ]; then \
		docker exec -i ocean-mysql mysql -uocean_user -pocean_pass ocean_db < docker/mysql/updates/latest.sql; \
		echo "✅ 업데이트 완료!"; \
	else \
		echo "⚠️  업데이트 파일이 없습니다: docker/mysql/updates/latest.sql"; \
		echo "📝 파일을 생성하거나 업데이트가 필요없다면 무시하세요."; \
	fi


fresh:
	@echo "🔄 DB 완전 초기화 중..."
	@cd docker && docker-compose down -v
	@cd docker && docker-compose up -d
	@echo "✅ 새로운 스키마로 재생성 완료!"

status:
	@echo "📊 Ocean 컨테이너 상태:"
	@docker ps --filter "name=ocean"

mysql:
	@echo "🔗 MySQL 접속 중..."
	@docker exec -it ocean-mysql mysql -uocean_user -pocean_pass ocean_db

logs:
	@cd docker && docker-compose logs -f