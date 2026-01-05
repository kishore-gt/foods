package com.srFoodDelivery.Controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.ChatMessage;
import com.srFoodDelivery.model.ChatRoom;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.chat.ChatService;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final MultiOrderRepository multiOrderRepository;
    private final SubOrderRepository subOrderRepository;

    public ChatController(
            ChatService chatService,
            MultiOrderRepository multiOrderRepository,
            SubOrderRepository subOrderRepository) {
        this.chatService = chatService;
        this.multiOrderRepository = multiOrderRepository;
        this.subOrderRepository = subOrderRepository;
    }

    @GetMapping
    public String chatList(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        List<ChatRoom> chatRooms = chatService.getChatRoomsForUser(user);
        Long unreadCount = chatService.getUnreadMessageCount(user);
        
        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("user", user);
        
        return "chat/list";
    }

    @GetMapping("/order/{orderId}")
    public String chatForOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        MultiOrder order = multiOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // Determine the other participant (could be rider or restaurant owner)
        User otherParticipant = null;
        if (order.getSubOrders() != null && !order.getSubOrders().isEmpty()) {
            SubOrder firstSubOrder = order.getSubOrders().get(0);
            if (firstSubOrder.getRider() != null && firstSubOrder.getRider().getUser() != null) {
                otherParticipant = firstSubOrder.getRider().getUser();
            } else if (firstSubOrder.getRestaurant() != null && 
                       firstSubOrder.getRestaurant().getOwner() != null) {
                otherParticipant = firstSubOrder.getRestaurant().getOwner();
            }
        }
        
        if (otherParticipant == null) {
            model.addAttribute("error", "No chat participant found for this order");
            return "chat/error";
        }
        
        List<ChatMessage> messages = chatService.getConversationForOrder(user, otherParticipant, order);
        ChatRoom chatRoom = chatService.getOrCreateChatRoom(user, otherParticipant, order, null);
        
        model.addAttribute("messages", messages);
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("order", order);
        model.addAttribute("otherParticipant", otherParticipant);
        model.addAttribute("user", user);
        
        return "chat/order";
    }

    @GetMapping("/suborder/{subOrderId}")
    public String chatForSubOrder(
            @PathVariable Long subOrderId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));
        
        // Determine the other participant
        User otherParticipant = null;
        if (subOrder.getRider() != null && subOrder.getRider().getUser() != null) {
            otherParticipant = subOrder.getRider().getUser();
        } else if (subOrder.getRestaurant() != null && 
                   subOrder.getRestaurant().getOwner() != null) {
            otherParticipant = subOrder.getRestaurant().getOwner();
        }
        
        if (otherParticipant == null) {
            model.addAttribute("error", "No chat participant found for this suborder");
            return "chat/error";
        }
        
        List<ChatMessage> messages = chatService.getConversation(user, otherParticipant, subOrder);
        ChatRoom chatRoom = chatService.getOrCreateChatRoom(user, otherParticipant, 
                subOrder.getMultiOrder(), subOrder);
        
        model.addAttribute("messages", messages);
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("subOrder", subOrder);
        model.addAttribute("otherParticipant", otherParticipant);
        model.addAttribute("user", user);
        
        return "chat/suborder";
    }

    @PostMapping("/send")
    public String sendMessage(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long subOrderId,
            @RequestParam String message,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User sender = requireUser(principal);
        User receiver = new User();
        receiver.setId(receiverId);
        
        MultiOrder order = orderId != null ? 
                multiOrderRepository.findById(orderId).orElse(null) : null;
        SubOrder subOrder = subOrderId != null ? 
                subOrderRepository.findById(subOrderId).orElse(null) : null;
        
        chatService.sendMessage(sender, receiver, message, order, subOrder);
        
        redirectAttributes.addFlashAttribute("successMessage", "Message sent!");
        
        if (subOrderId != null) {
            return "redirect:/chat/suborder/" + subOrderId;
        } else if (orderId != null) {
            return "redirect:/chat/order/" + orderId;
        } else {
            return "redirect:/chat";
        }
    }

    private User requireUser(CustomUserDetails principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return principal.getUser();
    }
}

