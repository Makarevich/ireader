issue_time_post = () -> window.jQuery.post("start-time")

repeater = (issue_post, initial_cb, next_cb) ->
    on_sched = null
    sched_next = () ->
        window.setTimeout(on_sched, 10000)
    on_sched = () ->
        issue_post().done(next_cb).always(sched_next)
    issue_post().done(initial_cb).done(sched_next)

window.setup_page_reloader = () ->
    timestamp = ""

    on_initial_time = (data) ->
        timestamp = data

    on_new_time = (data) ->
        if(data != timestamp)
            console.log("reloading!!")
            window.location.reload(true)

    repeater(issue_time_post, on_initial_time, on_new_time)
