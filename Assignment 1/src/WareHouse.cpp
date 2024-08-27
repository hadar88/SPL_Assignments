#include <iostream>
#include <sstream>
#include <fstream>
#include <algorithm>  
#include "WareHouse.h"
#include "Action.h"
using namespace std;
using std::vector;

WareHouse::WareHouse(const string &configFilePath): isOpen(false), actionsLog(0), volunteers(0), pendingOrders(0), inProcessOrders(0), completedOrders(0), customers(0), customerCounter(0), volunteerCounter(0), orderCounter(0){

    ifstream file(configFilePath);
    string line;

    while(getline(file, line)){
        stringstream linestream(line);
        string data;
        string name;
        string type;
        int val1;
        int val2;
        int val3;
        getline(linestream, data, ' ');
        if(data == "customer" || data == "volunteer"){
            linestream >> name >> type >> val1 >> val2 >> val3;
            if(data == "customer" && type == "soldier"){
                SoldierCustomer* temp = new SoldierCustomer(customerCounter, name, val1, val2);
                customerCounter++;
                customers.push_back(temp);
            }
            else if(data == "customer" && type == "civilian"){
                CivilianCustomer* temp = new CivilianCustomer(customerCounter, name, val1, val2);
                customerCounter++;
                customers.push_back(temp);
            }
            else if(data == "volunteer" && type == "collector"){
                CollectorVolunteer* temp = new CollectorVolunteer(volunteerCounter, name, val1);
                volunteerCounter++;
                volunteers.push_back(temp);
            }
            else if(data == "volunteer" && type == "limited_collector"){
                LimitedCollectorVolunteer* temp = new LimitedCollectorVolunteer(volunteerCounter, name, val1, val2);
                volunteerCounter++;
                volunteers.push_back(temp);
            }
            else if(data == "volunteer" && type == "driver"){
                DriverVolunteer* temp = new DriverVolunteer(volunteerCounter, name, val1, val2);
                volunteerCounter++;
                volunteers.push_back(temp);
            }
            else if(data == "volunteer" && type == "limited_driver"){
                LimitedDriverVolunteer* temp = new LimitedDriverVolunteer(volunteerCounter, name, val1, val2,val3);
                volunteerCounter++;
                volunteers.push_back(temp);
            }
            else{
                cerr << "Unknown type" <<endl;
            }
        }
    }
}

void WareHouse::start(){
    (*this).open();
    while(isOpen){
        string line;
        string act;
        string name;
        string type;
        int val1;
        int val2;
        getline(std::cin,line);
        stringstream linestream(line);
        getline(linestream, act, ' ');
        if(act == "customer"){
          linestream >> name >> type >> val1 >> val2;
          AddCustomer* customer = new AddCustomer(name,type, val1, val2);
          actionsLog.push_back(customer);
          customer->act(*this);
        }
        else if(act == "log"){
            PrintActionsLog* log = new PrintActionsLog();
            log->act(*this);
            actionsLog.push_back(log);
        }
        else if(act == "close"){
            Close* close = new Close();
            actionsLog.push_back(close);
            close->act(*this);
        }
        else if(act == "backup"){
            BackupWareHouse* backup = new BackupWareHouse();
            actionsLog.push_back(backup);
            backup->act(*this);
        }
        else if(act == "restore"){
            RestoreWareHouse* restore = new RestoreWareHouse();
            restore->act(*this);
            actionsLog.push_back(restore);
        }
        else{
            linestream >> val1;
            if(act == "step"){
                SimulateStep* step = new SimulateStep(val1);
                actionsLog.push_back(step);
                step->act(*this);
            }
            else if(act == "order"){
                AddOrder* order = new AddOrder(val1);
                actionsLog.push_back(order);
                order->act(*this);
            }
            else if(act == "orderStatus"){
                PrintOrderStatus* status = new PrintOrderStatus(val1);
                actionsLog.push_back(status);
                status->act(*this);
            }
            else if(act == "customerStatus"){
                PrintCustomerStatus* status = new PrintCustomerStatus(val1);
                actionsLog.push_back(status);
                status->act(*this);
            }
            else if(act == "volunteerStatus"){
                PrintVolunteerStatus* status = new PrintVolunteerStatus(val1);
                actionsLog.push_back(status);
                status->act(*this);
            }
        }
    }
}

void WareHouse::addOrder(Order* order){
    pendingOrders.push_back(order);
}

void WareHouse::addCustomer(Customer* customer){
    customers.push_back(customer);
}

void WareHouse::addInProcessOrders(Order* order) {
    inProcessOrders.push_back(order);
}

void WareHouse::addCompletedOrders(Order* order) {
    completedOrders.push_back(order);
}

void WareHouse::addAction(BaseAction* action){
    actionsLog.push_back(action);
}

Order WareHouse::DEFAULT_ORDER(-1,-1,-1);
CivilianCustomer WareHouse::DEFAULT_CUSTOMER(-1, "", -1, -1);
CollectorVolunteer WareHouse::DEFAULT_VOLUNTEER(-1, "", -1);

Customer &WareHouse::getCustomer(int customerId) const{
    for(Customer *customer : customers){
        if((*customer).getId() == customerId){
            return *customer;
        }
    }
    return (Customer &)this->DEFAULT_CUSTOMER;
}

Volunteer &WareHouse::getVolunteer(int volunteerId) const{
    for(Volunteer *volunteer : volunteers){
        if((*volunteer).getId() == volunteerId){
            return *volunteer;
        }
    }
    return (Volunteer &)this->DEFAULT_VOLUNTEER;
}

Order &WareHouse::getOrder(int orderId) const{
    for(Order *order : pendingOrders){
        if(order->getId() == orderId){
            return *order;
        }
    }
    
    for(Order *order : inProcessOrders){
        if(order->getId() == orderId){
            return *order;
        }
    }
    for(Order *order : completedOrders){
        if((*order).getId() == orderId){
            return *order;
        }
    }
    return (Order &)this->DEFAULT_ORDER;
}

const vector<BaseAction*> &WareHouse::getActions() const{
    return actionsLog;
}

const vector<Volunteer*> &WareHouse::getVolunteers() const{
    return volunteers;
}

const vector<Order*> &WareHouse::getPendingOrders() const{
    return pendingOrders;
}

const vector<Order*> &WareHouse::getInProcessOrders() const{
    return inProcessOrders;
}

const vector<Order*> &WareHouse::getCompletedOrders() const{
    return completedOrders;
}

void WareHouse::close(){
    isOpen=false;
    clear();
}

void WareHouse::clear(){
    for(BaseAction* action : actionsLog){
        if(action != nullptr){
            delete action;
        }
    }
    for(Volunteer* volunteer : volunteers){
        delete volunteer;
    }
    for(Order* order : pendingOrders){
        delete order;
    }
    for(Order* order : inProcessOrders){
        delete order;
    }
    for(Order* order : completedOrders){
        delete order;
    }
    for(Customer* customer : customers){
        delete customer;
    }
    actionsLog.clear();
    volunteers.clear();
    pendingOrders.clear();
    inProcessOrders.clear();
    completedOrders.clear();
    customers.clear();
}

void WareHouse::open(){
    isOpen=true;
    std::cout << "Warehouse is open!" << std::endl;
}

WareHouse::WareHouse(const WareHouse &other):
    isOpen( other.isOpen),
    actionsLog(0), 
    volunteers(0),  
    pendingOrders(0),  
    inProcessOrders(0), 
    completedOrders(0),  
    customers(0),
    customerCounter(other.customerCounter),
    volunteerCounter(other.volunteerCounter),
    orderCounter(other.orderCounter)
    {
    for(BaseAction* action : other.actionsLog){
            actionsLog.push_back(action->clone());
    }
    for(Volunteer* volunteer : other.volunteers){
        volunteers.push_back(volunteer->clone());
    }
    for(Order* order : other.pendingOrders){
        pendingOrders.push_back(order->clone());
    }
    for(Order* order : other.inProcessOrders){
        inProcessOrders.push_back(order->clone());
    }
    for(Order* order : other.completedOrders){
        completedOrders.push_back(order->clone());
    }
    for(Customer* customer : other.customers){
        customers.push_back(customer->clone());
    }
}

WareHouse::~WareHouse(){
    clear();
}

WareHouse& WareHouse::operator=(const WareHouse &other){
    if(this != &other){
        isOpen = other.isOpen;
        clear();
        for(BaseAction* action : other.actionsLog){
            actionsLog.push_back(action->clone());
        }
        for(Volunteer* volunteer : other.volunteers){
            volunteers.push_back(volunteer->clone());
        }
        for(Order* order : other.pendingOrders){
            pendingOrders.push_back(order->clone());
        }
        for(Order* order : other.inProcessOrders){
            inProcessOrders.push_back(order->clone());
        }
        for(Order* order : other.completedOrders){
            completedOrders.push_back(order->clone());
        }
        for(Customer* customer : other.customers){
            customers.push_back(customer->clone());
        }
        customerCounter = other.customerCounter;
        volunteerCounter = other.volunteerCounter;
        orderCounter = other.orderCounter;
    }
    return *this;
}

WareHouse::WareHouse(WareHouse&& other) noexcept
    :   isOpen(other.isOpen),
        actionsLog(std::move(other.actionsLog)),
        volunteers(std::move(other.volunteers)),
        pendingOrders(std::move(other.pendingOrders)),
        inProcessOrders(std::move(other.inProcessOrders)),
        completedOrders(std::move(other.completedOrders)),
        customers(std::move(other.customers)),
        customerCounter(other.customerCounter),
        volunteerCounter(other.volunteerCounter),
        orderCounter(other.orderCounter) {}

WareHouse& WareHouse::operator=(WareHouse&& other) noexcept{
    if(this != &other){
        isOpen = other.isOpen;
        customerCounter = other.customerCounter;
        volunteerCounter = other.volunteerCounter;
        orderCounter = other.orderCounter;
        clear();
        actionsLog = std::move(other.actionsLog);
        volunteers = std::move(other.volunteers);
        pendingOrders = std::move(other.pendingOrders);
        inProcessOrders = std::move(other.inProcessOrders);
        completedOrders = std::move(other.completedOrders);
        customers = std::move(other.customers);
    }
    return *this;   
}

void WareHouse::erasePendingOrders(int i){
    pendingOrders.erase(pendingOrders.begin()+i);
}

void WareHouse::eraseVolunteers(int i){
    volunteers.erase(volunteers.begin()+i);
}

void WareHouse::removeInProcessOrders(Order* thisOrder){
    for (auto it = inProcessOrders.begin(); it != inProcessOrders.end(); ++it) {
        if ((*it)->getId() == thisOrder->getId()) {
            inProcessOrders.erase(it);
            break;
        }
    }
}

int WareHouse::getOrderCounter() const{
    return orderCounter;
}

int WareHouse::getCustomerCounter() const{
    return customerCounter;
}

int WareHouse::getVolunteerCounter() const{
    return volunteerCounter;
}

void WareHouse::changeOrderCounter(){
    orderCounter++;
}

void WareHouse::changeCustomerCounter(){
    customerCounter++;
}

bool WareHouse::existOrder(int orderId) const{
    for(Order *order : pendingOrders){
        if(order->getId() == orderId){
            return true;
        }
    }
    for(Order *order : inProcessOrders){
        if(order->getId() == orderId){
            return true;
        }
    }
    for(Order *order : completedOrders){
        if((*order).getId() == orderId){
            return true;
        }
    }
    return false;
}

bool WareHouse::existVolunteer(int id) const{
    for(Volunteer *volunteer : volunteers){
        if((*volunteer).getId() == id){
            return true;
        }
    }
    return false;
}