-- 키워드 검색용 trigram 인덱스 (T16-4).
-- ddl-auto 로는 만들 수 없는 인덱스라 Hibernate 가 테이블을 만든 뒤 이 파일을 실행한다
-- (spring.jpa.defer-datasource-initialization=true). 서버를 켤 때마다 실행되므로 IF NOT EXISTS 로 둔다.
--
-- 제목·본문 ILIKE '%검색어%' 는 앞에 % 가 붙어 일반 B-tree 인덱스를 쓸 수 없다.
-- pg_trgm 은 글자를 3글자 조각으로 잘라 색인하므로 부분 일치 검색도 인덱스를 탄다.
-- 한계: 2글자 이하 검색어(예: "자바")는 3글자 조각이 없어 인덱스 효과가 거의 없다.
-- DB 로케일(LC_CTYPE)이 C 면 한글을 글자로 보지 않아 조각이 비므로, 이 파일은 UTF-8 로케일을 전제로 한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_posts_title_trgm ON posts USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_posts_content_trgm ON posts USING gin (content gin_trgm_ops);
