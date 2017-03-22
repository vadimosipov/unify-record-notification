DROP TABLE IF EXISTS dataport;
CREATE TABLE dataport
(
    datasourceid INT NOT NULL,
    recordtype INT,
    value VARCHAR(255) NOT NULL,
);