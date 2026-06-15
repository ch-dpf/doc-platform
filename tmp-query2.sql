SET client_encoding TO 'UTF8';
SELECT COUNT(*) AS with_submitter
FROM document_chunk
WHERE library_id='b3aa21d9-3062-410c-8714-bc69b945b924'
  AND metadata->>'periodYear'='2025'
  AND metadata->>'submitter'='杜鹏飞'
  AND metadata->>'periodStart' <= '2025-09-30'
  AND metadata->>'periodEnd' >= '2025-09-01';

SELECT COUNT(*) AS without_submitter
FROM document_chunk
WHERE library_id='b3aa21d9-3062-410c-8714-bc69b945b924'
  AND metadata->>'periodYear'='2025'
  AND metadata->>'periodStart' <= '2025-09-30'
  AND metadata->>'periodEnd' >= '2025-09-01';

SELECT DISTINCT metadata->>'submitter' AS submitter, length(metadata->>'submitter') AS len
FROM document_chunk
WHERE library_id='b3aa21d9-3062-410c-8714-bc69b945b924'
  AND metadata->>'periodYear'='2025'
  AND metadata->>'periodMonths'='9';
