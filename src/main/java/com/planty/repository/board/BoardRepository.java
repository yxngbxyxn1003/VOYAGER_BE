package com.planty.repository.board;

import com.planty.entity.board.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


// 판매 게시판 레포지토리
public interface BoardRepository extends JpaRepository<Board, Integer> {
    // 판매 게시글 상세 정보 가져오기
    @EntityGraph(attributePaths = {"user","crop","images"})
    Optional<Board> findById(Integer id);

    // 판매 게시글 전체 목록 가져오기
    List<Board> findAllByOrderByCreatedAtDesc();

    // 게시글 기준으로 crop 아이디 가져오기
    @Query("select b.crop.id from Board b where b.id = :boardId")
    Integer findCropIdByBoardId(@Param("boardId") Integer boardId);
}
