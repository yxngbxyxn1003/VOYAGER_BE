package com.planty.repository.board;

import com.planty.entity.board.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


// 판매 게시판 레포지토리
public interface BoardRepository extends JpaRepository<Board, Integer> {
    // 판매 게시글 상세 정보 가져오기
    @EntityGraph(attributePaths = {"user","crop","images"})
    Optional<Board> findById(Integer id);
}
