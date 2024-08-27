#include "Action.h"
#include "WareHouse.h"
#include <iostream>
using namespace std;

extern WareHouse* backup;

BaseAction::BaseAction():errorMsg(""), status(ActionStatus::ERROR){}

ActionStatus BaseAction::getStatus() const{
    return status;
}

void BaseAction::complete() {
    status = ActionStatus::COMPLETED;
}

void BaseAction::error(string errorMsg){
    status = ActionStatus::ERROR;
    (*this).errorMsg = errorMsg;
}

string BaseAction::getErrorMsg() const {
    return errorMsg;
}

void BaseAction::setStatus(ActionStatus other){
    if(other == ActionStatus::COMPLETED){
        status = ActionStatus::COMPLETED;
    }
    else{
        status = ActionStatus::ERROR;
    }
}

void BaseAction::setErrorMsg(string other){
    errorMsg = other;
}

const string BaseAction::change() const{
    switch (status) {
        case ActionStatus::COMPLETED: return "COMPLETED";
        case ActionStatus::ERROR: return "ERROR";
        default: return "Unknown";
    }
}

const CustomerType BaseAction::change1(const string &other) const{
    if(other == "soldier"){
        return CustomerType::Soldier;
    }
    else{
        return CustomerType::Civilian;
    }
}

const string BaseAction::change2(CustomerType other) const{
    switch (other) {
        case CustomerType::Soldier: return "soldier";
        case CustomerType::Civilian: return "civilian";
        default: return "Unknown";
    }
}





SimulateStep::SimulateStep(int numOfSteps): BaseAction(), numOfSteps(numOfSteps){}

void SimulateStep::act(WareHouse &wareHouse){
  for(int j = 1; j <= numOfSteps; j++){  
    int size = wareHouse.getPendingOrders().size();
    for (int i = 0; i < size; i++){
        Order* order = wareHouse.getPendingOrders()[i];
        for(Volunteer* volunteer : wareHouse.getVolunteers()){
            if (volunteer->canTakeOrder(*order)){
                volunteer->acceptOrder(*order);
                if(order->getStatus() == OrderStatus::COLLECTING){
                    order->setStatus(OrderStatus::DELIVERING);
                    order->setDriverId(volunteer->getId());
                }
                else{
                    order->setStatus(OrderStatus::COLLECTING);
                    order->setCollectorId(volunteer->getId());
                }
                wareHouse.erasePendingOrders(i);
                i--;
                size--;
                wareHouse.addInProcessOrders(order);
                break;
            } 
        }
    }
    int size1 = wareHouse.getVolunteers().size();
    for (int i = 0; i < size1; i++){
        Volunteer* volunteer = wareHouse.getVolunteers()[i];
        volunteer->step();
        if(volunteer->getCompletedNow()){
            Order &thisOrder = wareHouse.getOrder(volunteer->getCompletedOrderId());
            if(thisOrder.getStatus() == OrderStatus::COLLECTING){
                wareHouse.removeInProcessOrders(&thisOrder);
                wareHouse.addOrder(&thisOrder);
            }
            if(thisOrder.getStatus() == OrderStatus::DELIVERING){
                wareHouse.removeInProcessOrders(&thisOrder);
                wareHouse.addCompletedOrders(&thisOrder);
                thisOrder.setStatus(OrderStatus::COMPLETED);
            }
            if(!(volunteer->hasOrdersLeft())){
                delete volunteer;
                wareHouse.eraseVolunteers(i);
                i--;
                size1--;
            }
        }
    }
    (*this).complete();
  }
}


std::string SimulateStep::toString() const{
    return  "simulateStep " + to_string(numOfSteps) + " " + change();
}

SimulateStep *SimulateStep::clone() const{
    SimulateStep* copy = new SimulateStep(numOfSteps);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}





AddOrder::AddOrder(int id):BaseAction(),customerId(id){}

void AddOrder::act(WareHouse &wareHouse){
    if(customerId > wareHouse.getCustomerCounter() || customerId < 0 || !wareHouse.getCustomer(customerId).canMakeOrder()){
        error("Cannot place this order");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
    else{
        int distance = wareHouse.getCustomer(customerId).getCustomerDistance();
        int orderCounter = wareHouse.getOrderCounter();
        Order* order = new Order(orderCounter, customerId, distance);
        order->setStatus(OrderStatus::PENDING);
        wareHouse.addOrder(order);
        wareHouse.changeOrderCounter();
        wareHouse.getCustomer(customerId).addOrder(orderCounter);
        (*this).complete();
    }
}

string AddOrder::toString() const{
    return  "order " + to_string(customerId) + " " + change();
}

AddOrder *AddOrder::clone() const{
    AddOrder* copy = new AddOrder(customerId);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}





AddCustomer::AddCustomer(const string &customerName, const string &customerType, int distance, int maxOrders):
    BaseAction(), customerName(customerName), customerType(change1(customerType)), distance(distance), maxOrders(maxOrders){}

void AddCustomer::act(WareHouse &wareHouse){
    if(customerType == CustomerType::Civilian){
        CivilianCustomer* customer = new CivilianCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders);
        wareHouse.addCustomer(customer);
    }
    else{
        SoldierCustomer* customer = new SoldierCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders);
        wareHouse.addCustomer(customer);
    }
    wareHouse.changeCustomerCounter();
    (*this).complete();
}

AddCustomer *AddCustomer::clone() const{
    AddCustomer* copy = new AddCustomer(customerName, change2(customerType), distance, maxOrders);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string AddCustomer::toString() const{
    return  "customer " + customerName + " " + change2(customerType) + " " + to_string(distance) + " " + to_string(maxOrders) + " " + change();
}





PrintOrderStatus::PrintOrderStatus(int id):BaseAction(),orderId(id){}

void PrintOrderStatus::act(WareHouse &wareHouse){
    if(orderId > wareHouse.getOrderCounter() || orderId < 0 || !wareHouse.existOrder(orderId)){
        error("Order doesn't exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
    else{
        std::cout << wareHouse.getOrder(orderId).toString() << std::endl;
        (*this).complete();
    }
}

PrintOrderStatus *PrintOrderStatus::clone() const{
    PrintOrderStatus* copy = new PrintOrderStatus(orderId);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string PrintOrderStatus::toString() const{
    return  "orderStatus " + to_string(orderId) + " " + change();
}





PrintCustomerStatus::PrintCustomerStatus(int customerId):BaseAction(), customerId(customerId){}

void PrintCustomerStatus::act(WareHouse &wareHouse){
    if(customerId > wareHouse.getCustomerCounter() || customerId < 0){
        error("Customer doesn't exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
    else{
        string toString="";
        toString = toString + "CustomerID: " + to_string(customerId);
        for(int id : wareHouse.getCustomer(customerId).getOrdersIds()){
            toString = toString + "\nOrderID: " + to_string(id);
            toString = toString + "\nOrderStatus: " + wareHouse.getOrder(id).change(); 
        }
        toString = toString + "\nnumOrdersLeft: " + to_string(wareHouse.getCustomer(customerId).getMaxOrders() - wareHouse.getCustomer(customerId).getNumOrders());
        std::cout << toString << std::endl;
        (*this).complete();
    }
}

PrintCustomerStatus *PrintCustomerStatus::clone() const{
    PrintCustomerStatus* copy = new PrintCustomerStatus(customerId);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string PrintCustomerStatus::toString() const{
    return  "customerStatus " + to_string(customerId) + " " + change();
}





PrintVolunteerStatus::PrintVolunteerStatus(int id):BaseAction(), volunteerId(id){}

void PrintVolunteerStatus::act(WareHouse &wareHouse){
    if(volunteerId > wareHouse.getVolunteerCounter() || volunteerId < 0 || !wareHouse.existVolunteer(volunteerId)){
        error("Volunteer doesn't exist");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
    else{
        std::cout << wareHouse.getVolunteer(volunteerId).toString() << std::endl;
        (*this).complete();
    }
}

PrintVolunteerStatus *PrintVolunteerStatus::clone() const{
    PrintVolunteerStatus* copy = new PrintVolunteerStatus(volunteerId);
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string PrintVolunteerStatus::toString() const{
    return  "volunteerStatus " + to_string(volunteerId) + " " + change();
}





PrintActionsLog::PrintActionsLog():BaseAction(){}

void PrintActionsLog::act(WareHouse &wareHouse){
    for(BaseAction* action : wareHouse.getActions()){
        std::cout << action->toString() << "\n" << std::endl;
    }
    (*this).complete();
}

PrintActionsLog *PrintActionsLog::clone() const{
    PrintActionsLog* copy = new PrintActionsLog();
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string PrintActionsLog::toString() const{
    return  "log " + change();
}





Close::Close():BaseAction(){}

void Close::act(WareHouse &wareHouse){
    for(int i = 0; i < wareHouse.getOrderCounter(); i++){
       std::cout << "OrderID: " << wareHouse.getOrder(i).getId() << ", CustomerID: " << wareHouse.getOrder(i).getCustomerId() << ", OrderStatus: " << wareHouse.getOrder(i).change() << "\n" << std::endl;
    }
    (*this).complete();
    wareHouse.close();
}

Close *Close::clone() const{
    Close* copy = new Close();
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string Close::toString() const{
    return  "close " + change();
}






BackupWareHouse::BackupWareHouse():BaseAction(){}

void BackupWareHouse::act(WareHouse &wareHouse){
    (*this).complete();
    if(backup == nullptr){
        backup = new WareHouse(wareHouse);
    }
    else{
        (*backup) = wareHouse;
    }
    
}

BackupWareHouse *BackupWareHouse::clone() const{
    BackupWareHouse* copy = new BackupWareHouse();
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string BackupWareHouse::toString() const{
    return  "backup " + change();
}





RestoreWareHouse::RestoreWareHouse():BaseAction(){}

void RestoreWareHouse::act(WareHouse &wareHouse){
    if(backup != nullptr){
        
        wareHouse = *backup;
        (*this).complete();
    }
    else{
        error("No backup available");
        std::cout << "Error: " << getErrorMsg() << std::endl;
    }
}

RestoreWareHouse *RestoreWareHouse::clone() const{
    RestoreWareHouse* copy = new RestoreWareHouse();
    copy->setStatus(getStatus());
    copy->setErrorMsg(getErrorMsg());
    return copy;
}

string RestoreWareHouse::toString() const{
    return  "restore " + change();
}