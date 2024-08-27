# All Targets

all: bin/warehouse

# Tool invocations

bin/warehouse: clean bin/Order.o bin/Customer.o bin/Volunteer.o bin/WareHouse.o bin/Action.o bin/main.o

	g++ -o bin/warehouse bin/Order.o bin/Customer.o bin/Volunteer.o bin/WareHouse.o bin/Action.o bin/main.o

# Depends on the source and header files

bin/main.o: src/main.cpp include/WareHouse.h

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/main.o src/main.cpp

bin/WareHouse.o: src/WareHouse.cpp include/Volunteer.h include/Customer.h include/Order.h

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/WareHouse.o src/WareHouse.cpp

bin/Action.o: src/Action.cpp include/WareHouse.h

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Action.o src/Action.cpp

bin/Customer.o: src/Customer.cpp 

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Customer.o src/Customer.cpp

bin/Order.o: src/Order.cpp 

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Order.o src/Order.cpp

bin/Volunteer.o: src/Volunteer.cpp include/Order.h

	g++ -g -Wall -Weffc++ -std=c++11 -c -Iinclude -o bin/Volunteer.o src/Volunteer.cpp

#Clean the build directory

clean:
	rm -f bin/*
