#include "Order.h"
using namespace std;

Order::Order(int id, int customerId, int distance):id(id),customerId(customerId),distance(distance),status(OrderStatus::PENDING),collectorId(NO_VOLUNTEER),driverId(NO_VOLUNTEER){}

int Order::getId() const{
    return id;
}

int Order::getCustomerId() const{
    return customerId;
}

void Order::setStatus(OrderStatus status){
    (*this).status = status;
}

void Order::setCollectorId(int collectorId){
    (*this).collectorId = collectorId;
}

void Order::setDriverId(int driverId){
    (*this).driverId = driverId;
}

int Order::getCollectorId() const{
    return collectorId;
}

int Order::getDriverId() const{
    return driverId;
}

OrderStatus Order::getStatus() const{
    return (*this).status;
}

const string Order::toString() const{
    string a = "";
    if(collectorId == -1){
        a = "None";
    }
    else{
        a = to_string(collectorId);
    }
    string b = "";
    if(driverId == -1){
        b = "None";
    }
    else{
        b = to_string(driverId);
    }
    return "OrderId: " + to_string(id) + "\nOrderStatus: " + change() + "\nCustomerId: " + to_string(customerId) + "\nCollector: " + a + "\nDriver: " + b;
}

int Order::getDistance() const{
    return distance;
}

const string Order::change() const{
    switch (status) {
        case OrderStatus::PENDING: return "Pending";
        case OrderStatus::COLLECTING: return "Collecting";
        case OrderStatus::DELIVERING:  return "Delivering";
        case OrderStatus::COMPLETED:  return "Completed";
        default: return "Unknown";
    }
}

Order *Order::clone() const{
    Order* copy = new Order(id, customerId, distance);
    copy->setStatus(status);
    copy->collectorId = collectorId;
    copy->driverId = driverId;
    return copy;
}