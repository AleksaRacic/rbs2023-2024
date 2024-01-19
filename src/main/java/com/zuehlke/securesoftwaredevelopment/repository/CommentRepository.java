package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.domain.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CommentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CommentRepository.class);


    private DataSource dataSource;

    public CommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Comment comment) {
        String query = "insert into comments(giftId, userId, comment) values ( ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query);
        ) {
            pstmt.setInt(1, comment.getGiftId());
            pstmt.setInt(2, comment.getUserId());
            pstmt.setString(3, comment.getComment());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Could not create comment", e);
        }
    }

    public List<Comment> getAll(String giftId) {
        List<Comment> commentList = new ArrayList<>();
        String query = "SELECT giftId, userId, comment FROM comments WHERE giftId = " + giftId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                commentList.add(new Comment(rs.getInt(1), rs.getInt(2), rs.getString(3)));
            }
        } catch (SQLException e) {
            LOG.error("Could not get comments", e);
        }
        return commentList;
    }
}
