export function canReplyAt(maxReplyDepth: number | null, level: number): boolean {
  return maxReplyDepth === null || level + 2 <= maxReplyDepth;
}

export function canCommentOnDiscussion(maxReplyDepth: number | null): boolean {
  return maxReplyDepth === null || maxReplyDepth >= 1;
}
