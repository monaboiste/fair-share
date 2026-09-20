package com.github.monaboiste.fairshare.scoring.ast;

public record EventRule(Expression filterExpr, Expression scoreExpr) {}
