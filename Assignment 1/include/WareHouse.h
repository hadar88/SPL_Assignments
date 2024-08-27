#pragma once
#include <string>
#include <vector>

#include "Order.h"
#include "Customer.h"
#include "Volunteer.h"

class BaseAction;
class Volunteer;

// Warehouse responsible for Volunteers, Customers Actions, and Orders.


class WareHouse {

    public:
        WareHouse(const string &configFilePath);
        void start();
        void addOrder(Order* order);
        void addCustomer(Customer* customer);
        void addAction(BaseAction* action);
        Customer &getCustomer(int customerId) const;
        Volunteer &getVolunteer(int volunteerId) const;
        Order &getOrder(int orderId) const;
        const vector<BaseAction*> &getActions() const;
        const vector<Volunteer*> &getVolunteers() const;
        const vector<Order*> &getPendingOrders() const;
        const vector<Order*> &getInProcessOrders() const;
        const vector<Order*> &getCompletedOrders() const;
        void close();
        void open();

        void addInProcessOrders(Order* order);
        void addCompletedOrders(Order* order);
        void erasePendingOrders(int i);
        void eraseVolunteers(int i);
        void removeInProcessOrders(Order* thisOrder);
        int getOrderCounter() const;
        int getCustomerCounter() const;
        int getVolunteerCounter() const;
        void changeOrderCounter();
        void changeCustomerCounter();
        void clear();
        bool existOrder(int orderId) const;
        bool existVolunteer(int id) const;
        

        WareHouse(const WareHouse &other);
        ~WareHouse();
        WareHouse& operator=(const WareHouse &other);
        WareHouse(WareHouse&& other) noexcept;
        WareHouse& operator=(WareHouse&& other) noexcept;

    private:
        bool isOpen;
        vector<BaseAction*> actionsLog;
        vector<Volunteer*> volunteers;
        vector<Order*> pendingOrders;
        vector<Order*> inProcessOrders;
        vector<Order*> completedOrders;
        vector<Customer*> customers;
        int customerCounter; //For assigning unique customer IDs
        int volunteerCounter; //For assigning unique volunteer IDs
        int orderCounter; //For assigning unique order IDs

        static Order DEFAULT_ORDER;
        static CivilianCustomer DEFAULT_CUSTOMER;
        static CollectorVolunteer DEFAULT_VOLUNTEER;

        
};
