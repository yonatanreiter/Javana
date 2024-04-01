package edu.yu.compilers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class OfficialTest {

    private final static Logger logger = LogManager.getLogger(OfficialTest.class);

    static {
        Configurator.setLevel("edu.yu.compilers", Level.INFO);
    }

    @BeforeEach
    void setUp() {
        logger.info("Starting test");
    }

    @AfterEach
    void tearDown() {
        logger.info("Finished test");
    }

}