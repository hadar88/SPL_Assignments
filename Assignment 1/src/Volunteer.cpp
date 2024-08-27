#include "Volunteer.h"
#include "Order.h"
using namespace std;

Volunteer::Volunteer(int id, const string &name):completedOrderId(NO_ORDER), activeOrderId(NO_ORDER), completedNow(false),id(id), name(name){}

int Volunteer::getId() const{
    return id;
}

const string &Volunteer::getName() const{
    return name;
}

int Volunteer::getActiveOrderId() const{
    return activeOrderId;
}

int Volunteer::getCompletedOrderId() const{
    return completedOrderId;
}

bool Volunteer::isBusy() const{
    return activeOrderId!=NO_ORDER;
}

bool Volunteer::getCompletedNow() const{
    return completedNow;
}

 



 CollectorVolunteer::CollectorVolunteer(int id, const string &name, int coolDown):Volunteer(id, name), coolDown(coolDown), timeLeft(NO_ORDER){}

CollectorVolunteer *CollectorVolunteer::clone() const{
    CollectorVolunteer* copy = new CollectorVolunteer(getId(), getName(), coolDown);
    copy->timeLeft = timeLeft;
    copy->completedOrderId = completedOrderId;
    copy->activeOrderId = activeOrderId;
    copy->completedNow = completedNow;
    return copy;
}

void CollectorVolunteer::step(){
    completedNow = false;
    if(isBusy()){
        if((*this).decreaseCoolDown()){
            completedOrderId=activeOrderId;
            activeOrderId = NO_ORDER;
            completedNow = true;
        }
    }
}

int CollectorVolunteer::getCoolDown() const{
    return coolDown;
}

int CollectorVolunteer::getTimeLeft() const{
    return timeLeft;
}

void CollectorVolunteer::setTimeLeft(int setTimeLeft){
    timeLeft = setTimeLeft;
}

bool CollectorVolunteer::decreaseCoolDown(){
    if(timeLeft>0){
        timeLeft--;
    }
    if(timeLeft == 0){
        timeLeft = NO_ORDER;
    }

    return timeLeft==NO_ORDER;
}

bool CollectorVolunteer::hasOrdersLeft() const {
    return true;
}

bool CollectorVolunteer::canTakeOrder(const Order &order) const{
    
    OrderStatus status = OrderStatus::PENDING;
    if(order.getStatus() == status){
        return !isBusy();
    }
    return false;
}

void CollectorVolunteer::acceptOrder(const Order &order){
        activeOrderId = order.getId();
        timeLeft = coolDown;
}

string CollectorVolunteer::toString() const{
    string busy="";
    if(isBusy()){
        busy = "True";
    }
    else{
        busy = "False";
    }
    return "VolunterrID: " + to_string(getId()) + "\nisBusy: " + busy + "\norderID: " + to_string(activeOrderId) + "\nTimeLeft: " + to_string(timeLeft) + "\nOrdersLeft: No Limit";
}





LimitedCollectorVolunteer::LimitedCollectorVolunteer(int id, const string &name, int coolDown ,int maxOrders):CollectorVolunteer(id,name,coolDown),maxOrders(maxOrders),ordersLeft(maxOrders){}

LimitedCollectorVolunteer *LimitedCollectorVolunteer::clone() const {
    LimitedCollectorVolunteer* copy = new LimitedCollectorVolunteer(getId(),getName(),getCoolDown(),maxOrders);
    copy->setTimeLeft(getTimeLeft());
    copy->ordersLeft = ordersLeft;
    copy->completedOrderId = completedOrderId;
    copy->activeOrderId = activeOrderId;
    copy->completedNow = completedNow;
    return copy;
}

bool LimitedCollectorVolunteer::hasOrdersLeft() const{
    return ordersLeft!=0;
}

bool LimitedCollectorVolunteer::canTakeOrder(const Order &order) const{
    OrderStatus status = OrderStatus::PENDING;
    if((&order)->getStatus() == status){
        if(hasOrdersLeft()){
            return !isBusy();
        }
        else{
            return false;
        }
    }
    return false;
}

void LimitedCollectorVolunteer::acceptOrder(const Order &order){
    if(canTakeOrder(order)){
        activeOrderId = order.getId();
        setTimeLeft(getCoolDown());
        ordersLeft--;
    }
}

int LimitedCollectorVolunteer::getMaxOrders() const{
    return maxOrders;
}

int LimitedCollectorVolunteer::getNumOrdersLeft() const{
    return ordersLeft;
}

string LimitedCollectorVolunteer::toString() const{
    string busy="";
    if(isBusy()){
        busy = "True";
    }
    else{
        busy = "False";
    }
    return "VolunterrID: " + to_string(getId()) + "\nisBusy: " + busy + "\norderID: " + to_string(activeOrderId) + "\nTimeLeft: " + to_string(getTimeLeft()) + "\nOrdersLeft: " + to_string(ordersLeft); 
}





DriverVolunteer::DriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep):Volunteer(id, name), maxDistance(maxDistance), distancePerStep(distancePerStep),distanceLeft(NO_ORDER){}

DriverVolunteer *DriverVolunteer::clone() const{
    DriverVolunteer* copy = new DriverVolunteer(getId(), getName(), maxDistance, distancePerStep);
    copy->completedOrderId = completedOrderId;
    copy->activeOrderId = activeOrderId;
    copy->completedNow = completedNow;
    copy->distanceLeft = distanceLeft;
    return copy;
}

int DriverVolunteer::getDistanceLeft() const{
    return distanceLeft;
}

void DriverVolunteer::setDistanceLeft(int otherDistanceLeft){
    distanceLeft = otherDistanceLeft;
}

int DriverVolunteer::getMaxDistance() const{
    return maxDistance;
}

int DriverVolunteer::getDistancePerStep() const{
    return distancePerStep;
}  

bool DriverVolunteer::decreaseDistanceLeft(){
    distanceLeft = distanceLeft - distancePerStep;
    if(distanceLeft <= 0){
        distanceLeft = 0;
        return true;
    }
    return false;
} 

bool DriverVolunteer::hasOrdersLeft() const{
    return true;
}

bool DriverVolunteer::canTakeOrder(const Order &order) const{
    if(order.getStatus() == OrderStatus::COLLECTING){
        if(!isBusy()){
            if(order.getDistance() <= maxDistance){
                return true;
            }
            return false;
        }
        return false;
    }
    return false;
} 

void DriverVolunteer::acceptOrder(const Order &order){
    if(canTakeOrder(order)){
        distanceLeft = order.getDistance();
        activeOrderId = order.getId();
    }
}

void DriverVolunteer::step(){
    completedNow = false;
    if(isBusy()){
        if(decreaseDistanceLeft()){
            completedOrderId=activeOrderId;
            activeOrderId = NO_ORDER;
            completedNow = true;
        }
    }
}

string DriverVolunteer::toString() const{
    string busy="";
    if(isBusy()){
        busy = "True";
    }
    else{
        busy = "False";
    }
    return "VolunterrID: " + to_string(getId()) + "\nisBusy: " + busy + "\norderID: " + to_string(activeOrderId) + "\nTimeLeft: " + to_string(distanceLeft) + "\nOrdersLeft: No Limit";
}





LimitedDriverVolunteer::LimitedDriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep,int maxOrders):DriverVolunteer(id,name,maxDistance,distancePerStep),maxOrders(maxOrders),ordersLeft(maxOrders){}

LimitedDriverVolunteer *LimitedDriverVolunteer::clone() const{
    LimitedDriverVolunteer* copy = new LimitedDriverVolunteer(getId(), getName(), getMaxDistance(), getDistancePerStep(), getMaxOrders());
    copy->ordersLeft = ordersLeft;
    copy->completedOrderId = completedOrderId;
    copy->activeOrderId = activeOrderId;
    copy->completedNow = completedNow;
    copy->setDistanceLeft(getDistanceLeft());
    return copy;
}

int LimitedDriverVolunteer::getMaxOrders() const{
    return maxOrders;
}

int LimitedDriverVolunteer::getNumOrdersLeft() const{
    return ordersLeft;
}

bool LimitedDriverVolunteer::hasOrdersLeft() const{
     return ordersLeft!=0;
}

bool LimitedDriverVolunteer::canTakeOrder(const Order &order) const{
    if(order.getStatus() == OrderStatus::COLLECTING){
        if(!isBusy()){
            if(order.getDistance() <= getMaxDistance()){
                return true;
            }
            return false;
        }
        return false;
    }
    return false;
}

void LimitedDriverVolunteer::acceptOrder(const Order &order){
    if(canTakeOrder(order)){
        setDistanceLeft(order.getDistance());
        activeOrderId = order.getId();
        ordersLeft--;
    }
}

string LimitedDriverVolunteer::toString() const{
    string busy="";
    if(isBusy()){
        busy = "True";
    }
    else{
        busy = "False";
    }
    return "VolunterrID: " + to_string(getId()) + "\nisBusy: " + busy + "\norderID: " + to_string(activeOrderId) + "\nTimeLeft: " + to_string(getDistanceLeft()) + "\nOrdersLeft: " + to_string(ordersLeft);
}
