local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowSize = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])

local windowStart = now - windowSize

redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

local count = tonumber(redis.call('ZCOUNT', key, windowStart, now))

if count < maxRequests then
    redis.call('ZADD', key, now, now)
    redis.call('EXPIRE', key, math.ceil(windowSize / 1000))
    return 1
else
    return 0
end