CREATE TABLE orders (
    tenant       VARCHAR(40) NOT NULL,
    order_number VARCHAR(20) NOT NULL,
    customer_id  VARCHAR(20) NOT NULL,
    status       VARCHAR(60) NOT NULL,
    total        DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (tenant, order_number)
);

CREATE TABLE refund_request (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant      VARCHAR(40) NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    invoice     VARCHAR(20) NOT NULL,
    amount      DECIMAL(10, 2) NOT NULL,
    reason      VARCHAR(200) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    decided_by  VARCHAR(40)
);
