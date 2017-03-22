package ru.cti.iss.verint.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.cti.iss.verint.model.Channel;
import ru.cti.iss.verint.model.RecordType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VerintDao {

    private final int datasourceId;
    private final JdbcTemplate jdbcTemplate;

    public VerintDao(DataSource dataSource, int datasourceId) {
        this.jdbcTemplate = new JdbcTemplate (dataSource);
        this.datasourceId = datasourceId;
    }

    public List<Channel> findAll() {
        final String sql = "SELECT value, recordtype FROM dataport WHERE datasourceid = ?";
        return jdbcTemplate.query(sql, this::extractChannel, datasourceId);
    }

    public List<Channel> findByExtension(String extension) {
        final String sql = "SELECT value, recordtype FROM dataport WHERE datasourceid = ? AND value = ?";
        return jdbcTemplate.query(sql, this::extractChannel, datasourceId, extension);
    }

    private Channel extractChannel(ResultSet rs, int rowNum) throws SQLException {
        String value = rs.getString(1);
        int type = rs.getInt(2);
        return new Channel(value, RecordType.cast(type));
    }

/*    public String getVerintKey() throws IOException {
        //verint instance check
        return jdbcTemplate.query("select value from BPCONFIG where NAME = 'wfo/suid'",
                rs -> rs.next() ? rs.getString("value") : null);
    }*/
}
