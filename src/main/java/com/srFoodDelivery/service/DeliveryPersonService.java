package com.srFoodDelivery.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.DeliveryPerson;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.DeliveryPersonRepository;
import com.srFoodDelivery.repository.OrderRepository;

@Service
@Transactional
public class DeliveryPersonService {

    private final DeliveryPersonRepository deliveryPersonRepository;
    private final OrderRepository orderRepository;

    public DeliveryPersonService(DeliveryPersonRepository deliveryPersonRepository,
                                 OrderRepository orderRepository) {
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.orderRepository = orderRepository;
    }

    public DeliveryPerson getOrCreateDeliveryPerson(User user) {
        return deliveryPersonRepository.findByUser(user)
                .orElseGet(() -> {
                    DeliveryPerson dp = new DeliveryPerson();
                    dp.setUser(user);
                    dp.setPhoneNumber("");
                    dp.setIsAvailable(true);
                    return deliveryPersonRepository.save(dp);
                });
    }

    public DeliveryPerson updateDeliveryPerson(User user, String phoneNumber, String address) {
        DeliveryPerson dp = getOrCreateDeliveryPerson(user);
        dp.setPhoneNumber(phoneNumber);
        dp.setAddress(address);
        return deliveryPersonRepository.save(dp);
    }

    public Order assignDeliveryPerson(Long orderId, Long deliveryPersonId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery person not found"));

        if (!deliveryPerson.getIsAvailable()) {
            throw new IllegalStateException("Delivery person is not available");
        }

        order.setDeliveryPerson(deliveryPerson);
        order.setStatus(com.srFoodDelivery.model.OrderStatus.OUT_FOR_DELIVERY);
        order.setTrackingInfo("Order assigned to delivery person: " + deliveryPerson.getUser().getFullName());

        return orderRepository.save(order);
    }

    public Order updateTrackingInfo(Long orderId, String trackingInfo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setTrackingInfo(trackingInfo);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<DeliveryPerson> getAvailableDeliveryPersons() {
        return deliveryPersonRepository.findByIsAvailableTrue();
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryPerson> findByUser(User user) {
        return deliveryPersonRepository.findByUser(user);
    }
}

