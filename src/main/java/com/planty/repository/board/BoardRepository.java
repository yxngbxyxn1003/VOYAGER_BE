package com.planty.repository.board;

import com.planty.entity.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;


// 판매 게시판 레포지토리
public interface BoardRepository extends JpaRepository<Board, Integer> {
}
