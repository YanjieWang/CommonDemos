DIR_INC = ./include
DIR_SRC = ./src
DIR_OBJ = ./obj
DIR_BIN = ./out
TARGET=test
BIN_TARGET = ${DIR_BIN}/${TARGET}
src=$(wildcard ${DIR_SRC}/*.cpp)

$(TARGET):$(src) |$(DIR_BIN)
	g++ -o $(BIN_TARGET) -I$(DIR_INC) $(src);$(BIN_TARGET)

$(DIR_BIN):
	mkdir $(DIR_BIN)

clean:
	rm -rf $(DIR_BIN)
