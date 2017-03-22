package ru.cti.sipphone.pool;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class __SlfLogger implements net.sourceforge.peers.Logger {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public __SlfLogger(String loggerMdcKey) {
        //todo можно добавить какой-то идентификатор логгера
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Exception e) {
        logger.error(s, e);
    }

    @Override
    public void traceNetwork(String s, String s2) {
        logger.trace(s, s2);
    }

}
