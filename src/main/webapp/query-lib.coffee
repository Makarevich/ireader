
window.get_query_params = () ->
    q = window.location.search
    kvs = {}
    for part in q.substr(1).split('&')
        [k, v] = part.split('=')
        kvs[k] = v
    kvs

window.build_query_string = (kvs) ->
    '?' + ("#{k}=#{v}" for k, v of kvs).join('&')
