package com.demetrius.tribunal.financesettlement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tribunal-finance-settlement-service 启动类（金融结算系统/资金结算中枢）。
 */
@SpringBootApplication
@MapperScan("com.demetrius.tribunal.financesettlement.infrastructure.mapper")
public class FinanceSettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceSettlementServiceApplication.class, args);
    }
}
