package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.board.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
