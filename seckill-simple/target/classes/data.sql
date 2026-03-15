INSERT INTO product (name, stock, price)
SELECT '机械键盘', 50, 399.00
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '机械键盘');

INSERT INTO product (name, stock, price)
SELECT '无线鼠标', 100, 129.00
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '无线鼠标');
