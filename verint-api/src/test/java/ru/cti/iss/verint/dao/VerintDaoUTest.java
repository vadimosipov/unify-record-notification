package ru.cti.iss.verint.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.cti.iss.verint.TestVerintConfiguration;
import ru.cti.iss.verint.model.Channel;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestVerintConfiguration.class)
@ActiveProfiles("test")
public class VerintDaoUTest extends AbstractJUnit4SpringContextTests {

    @Inject
    private VerintDao verintDao;

    @Test
    public void shouldFindAllChannels() {
        assertEquals(3, verintDao.findAll().size());
    }

    @Test
    public void shouldQueryChannelsByExtension() {
        final int extension = ThreadLocalRandom.current().nextInt(1000, 10_000);
        final List<Channel> channels = verintDao.findByExtension(String.valueOf(extension));
        assertEquals(0, channels.size());
    }

    @Test
    public void shouldQueryNoChannelsByExtension() {
        final int extension = 1192;
        final List<Channel> channels = verintDao.findByExtension(String.valueOf(extension));
        assertEquals(1, channels.size());
    }
}
