-- Initial data for testing
-- This file is loaded automatically by Spring Boot when using H2

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, category, sku, active, created_at, updated_at)
VALUES 
('Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 50, 'Electronics', 'LAPTOP-PRO-15', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Wireless Mouse', 'Ergonomic wireless mouse with Bluetooth', 29.99, 200, 'Electronics', 'MOUSE-WIRELESS', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('USB-C Hub', 'Multi-port USB-C hub with HDMI', 49.99, 150, 'Electronics', 'USBC-HUB-7P', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mechanical Keyboard', 'RGB mechanical gaming keyboard', 89.99, 75, 'Electronics', 'KB-MECH-RGB', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Office Chair', 'Ergonomic office chair with lumbar support', 299.99, 25, 'Furniture', 'CHAIR-ERGO-01', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Standing Desk', 'Electric height-adjustable standing desk', 599.99, 15, 'Furniture', 'DESK-STAND-01', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Monitor 27 inch', '4K Ultra HD monitor 27 inch', 399.99, 40, 'Electronics', 'MON-27-4K', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Webcam HD', '1080p HD webcam with microphone', 79.99, 100, 'Electronics', 'WEBCAM-HD-1080', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
