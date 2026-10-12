CREATE TABLE orders (
    order_number VARCHAR(20) PRIMARY KEY,
    customer_id  VARCHAR(20) NOT NULL,
    status       VARCHAR(60) NOT NULL,
    total        DECIMAL(10, 2) NOT NULL
);

CREATE TABLE product (
    sku       VARCHAR(20) PRIMARY KEY,
    name      VARCHAR(80) NOT NULL,
    category  VARCHAR(20) NOT NULL,
    price     DECIMAL(10, 2) NOT NULL,
    weight_kg DECIMAL(5, 2) NOT NULL,
    rating    DECIMAL(2, 1) NOT NULL
);

CREATE TABLE refund_request (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    invoice     VARCHAR(20) NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    reason      VARCHAR(200) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    decided_by  VARCHAR(40)
);
