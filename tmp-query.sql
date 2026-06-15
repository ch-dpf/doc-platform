SET client_encoding TO 'UTF8';
SELECT metadata->>'periodStart' as ps,
       metadata->>'periodEnd' as pe,
       metadata->>'periodMonths' as pm,
       metadata->>'submitter' as s,
       metadata->>'hasCompletedWork' as hc,
       metadata->>'sectionLabel' as sl
FROM document_chunk
WHERE library_id='b3aa21d9-3062-410c-8714-bc69b945b924'
  AND metadata->>'periodYear'='2025'
  AND metadata->>'periodMonths'='9'
  AND metadata->>'hasCompletedWork'='true'
LIMIT 8;
