UPDATE device_limits
SET extra_device_ids = '[]'
WHERE extra_device_ids IS NULL
   OR extra_device_ids = ''
   OR extra_device_ids = 'null';