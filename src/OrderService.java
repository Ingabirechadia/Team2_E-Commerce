import java.util.List;

public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    public Order createOrder(String orderId, String customerId) {
        return new Order(orderId, customerId);
    }

    public void addItemToOrder(Order order, String productId, int quantity) 
            throws ProductNotFoundException {
        Product product = inventoryService.getProduct(productId);
        OrderItem item = new OrderItem(product, quantity);
        order.addItem(item);
    }

    public void processOrder(Order order) 
            throws InvalidOrderException, ProductNotFoundException, InsufficientStockException {
        
        if (order == null) {
            throw new InvalidOrderException("Order cannot be null");
        }

        if (order.isEmpty()) {
            order.setStatus(OrderStatus.REJECTED);
            throw new InvalidOrderException("Cannot process empty order: " + order.getOrderId());
        }

        order.setStatus(OrderStatus.PROCESSING);

        try {
            validateOrder(order);
            deductInventory(order);
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.add(order);
        } catch (ProductNotFoundException | InsufficientStockException e) {
            order.setStatus(OrderStatus.REJECTED);
            throw e;
        }
    }

    private void validateOrder(Order order) throws ProductNotFoundException, InsufficientStockException {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            
            if (!inventoryService.checkStockAvailability(product.getProductId(), item.getQuantity())) {
                throw new InsufficientStockException(
                    "Insufficient stock for product: " + product.getName() + 
                    ". Available: " + product.getStockLevel() + 
                    ", Requested: " + item.getQuantity()
                );
            }
        }
    }

    private void deductInventory(Order order) throws InsufficientStockException {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.reduceStock(item.getQuantity());
        }
    }

    public Order getOrder(String orderId) throws InvalidOrderException {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidOrderException("Order not found with ID: " + orderId));
    }

    public List<Order> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getAllOrders() {
        return List.copyOf(orderRepository.findAll());
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
