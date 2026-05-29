UPDATE device_limits
SET extra_device_ids = '[]'::jsonb
WHERE extra_device_ids IS NULL
   OR extra_device_ids::text = ''
   OR extra_device_ids::text = 'null';