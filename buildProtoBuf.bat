

bin\protoc --proto_path=resources --java_out=src resources/message.proto
bin\protoc --proto_path=resources --java_out=src resources/resource.proto

bin\protoc --proto_path=resources --java_out=src resources/services.proto