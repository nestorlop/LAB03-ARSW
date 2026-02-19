package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@Primary
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final JdbcTemplate jdbcTemplate;

    public PostgresBlueprintPersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        try {
            jdbcTemplate.update(
                    "INSERT INTO blueprints(author, name) VALUES (?, ?)",
                    bp.getAuthor(), bp.getName()
            );

            for (Point p : bp.getPoints()) {
                jdbcTemplate.update(
                        "INSERT INTO points(author, blueprint_name, x, y) VALUES (?, ?, ?, ?)",
                        bp.getAuthor(), bp.getName(), p.x(), p.y()
                );
            }

        } catch (Exception e) {
            throw new BlueprintPersistenceException("Error saving blueprint");
        }
    }

    @Override
    public Blueprint getBlueprint(String author, String name)
            throws BlueprintNotFoundException {

        List<Point> points = jdbcTemplate.query(
                "SELECT x, y FROM points WHERE author=? AND blueprint_name=?",
                (rs, rowNum) -> new Point(
                        rs.getInt("x"),
                        rs.getInt("y")
                ),
                author, name
        );

        if (points.isEmpty()) {
            throw new BlueprintNotFoundException("Blueprint not found");
        }

        return new Blueprint(author, name, points);
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author)
            throws BlueprintNotFoundException {

        List<String> names = jdbcTemplate.queryForList(
                "SELECT name FROM blueprints WHERE author=?",
                String.class,
                author
        );

        if (names.isEmpty()) {
            throw new BlueprintNotFoundException("Author not found");
        }

        Set<Blueprint> result = new HashSet<>();

        for (String name : names) {
            result.add(getBlueprint(author, name));
        }

        return result;
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT author, name FROM blueprints"
        );

        Set<Blueprint> result = new HashSet<>();

        for (Map<String, Object> row : rows) {
            String author = (String) row.get("author");
            String name = (String) row.get("name");
            try {
                result.add(getBlueprint(author, name));
            } catch (BlueprintNotFoundException ignored) {}
        }

        return result;
    }

    @Override
    public void addPoint(String author, String name, int x, int y)
            throws BlueprintNotFoundException {

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM blueprints WHERE author=? AND name=?",
                Integer.class,
                author, name
        );

        if (exists == null || exists == 0) {
            throw new BlueprintNotFoundException("Blueprint not found");
        }

        jdbcTemplate.update(
                "INSERT INTO points(author, blueprint_name, x, y) VALUES (?, ?, ?, ?)",
                author, name, x, y
        );
    }
}
