-- Migration: 20261010000001_fixed_function_search_paths.sql
-- Description: The two trigger functions ran with the caller's search_path, which Supabase's
-- Security Advisor flags (function_search_path_mutable): a role that could put its own objects
-- earlier on the path could change what they resolve. Pin them, like claim_ai_proxy_call and
-- delete_user_account already are. Neither looks anything up outside its own row, so behaviour
-- doesn't change.

ALTER FUNCTION public.check_display_name_moderation() SET search_path = '';
ALTER FUNCTION public.handle_updated_at() SET search_path = '';
