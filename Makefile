DOCKER_REPO=registry.cn-shanghai.aliyuncs.com
IMAGE_NAME=snb-deal
VERSION=""


build:
	if [ ! -d "target" ]; then mkdir target;fi
	cp -f /server/package/release/$(IMAGE_NAME)/$(VERSION).jar target/$(VERSION).jar
	docker build -t "$(DOCKER_REPO)/snb/$(IMAGE_NAME):$(VERSION)" .
push:
	docker push "$(DOCKER_REPO)/snb/$(IMAGE_NAME):$(VERSION)"
	docker rmi "$(DOCKER_REPO)/snb/$(IMAGE_NAME):$(VERSION)"