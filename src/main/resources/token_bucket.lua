local tokens_key = KEYS[1]
local refill_key = KEYS[2]

local capacity   = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])

local tokens     = tonumber(redis.call('GET', tokens_key))
local lastRefill = tonumber(redis.call('GET', refill_key))

if tokens == nil then
    tokens = capacity
end
if lastRefill == nil then
    lastRefill = now
end

local elapsed   = (now - lastRefill) / 1000.0
local newTokens = elapsed * refillRate
tokens = math.min(capacity, tokens + newTokens)

local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

redis.call('SET', tokens_key, tostring(tokens))
redis.call('SET', refill_key, tostring(now))
redis.call('EXPIRE', tokens_key, 600)
redis.call('EXPIRE', refill_key, 600)

return allowed