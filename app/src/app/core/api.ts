export interface AuthorResponse {
  id: string;
  username: string;
}

export interface UserResponse {
  id: string;
  username: string;
  maxReplyDepth: number | null;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface SettingsResponse {
  maxReplyDepth: number | null;
}

export interface DiscussionSummary {
  id: string;
  title: string;
  content: string;
  author: AuthorResponse;
  commentCount: number;
  createdAt: string;
}

export interface CommentResponse {
  id: string;
  parentId: string | null;
  content: string;
  author: AuthorResponse;
  createdAt: string;
  replies: CommentResponse[];
}

export interface DiscussionResponse {
  id: string;
  title: string;
  content: string;
  author: AuthorResponse;
  createdAt: string;
  comments: CommentResponse[];
}

export interface FieldValidationError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: FieldValidationError[];
}

export const API_BASE = '/api';
