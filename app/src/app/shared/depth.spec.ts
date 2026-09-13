import { describe, expect, it } from 'vitest';
import { canCommentOnDiscussion, canReplyAt } from './depth';

describe('depth helpers', () => {
  describe('canReplyAt', () => {
    it('returns true when unlimited', () => {
      expect(canReplyAt(null, 0)).toBe(true);
      expect(canReplyAt(null, 5)).toBe(true);
    });

    it('allows reply when resulting level is within limit', () => {
      // comments array at level 0 represents actual level 1
      expect(canReplyAt(3, 0)).toBe(true); // reply -> level 2
      expect(canReplyAt(3, 1)).toBe(true); // reply -> level 3
      expect(canReplyAt(5, 2)).toBe(true); // reply -> level 4 within 5
    });

    it('blocks reply when resulting level would exceed limit', () => {
      expect(canReplyAt(3, 2)).toBe(false); // reply -> level 4 > 3
      expect(canReplyAt(3, 3)).toBe(false);
      expect(canReplyAt(5, 4)).toBe(false); // reply -> level 6 > 5
    });

    it('handles maxReplyDepth 0 (no comments allowed) correctly', () => {
      expect(canReplyAt(0, 0)).toBe(false);
    });
  });

  describe('canCommentOnDiscussion', () => {
    it('allows direct comment when limit is at least 1 or unlimited', () => {
      expect(canCommentOnDiscussion(null)).toBe(true);
      expect(canCommentOnDiscussion(1)).toBe(true);
      expect(canCommentOnDiscussion(3)).toBe(true);
    });

    it('blocks direct comment when maxReplyDepth is 0', () => {
      expect(canCommentOnDiscussion(0)).toBe(false);
    });
  });
});
