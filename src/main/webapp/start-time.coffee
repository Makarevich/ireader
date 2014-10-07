issue_time_post = () -> window.jQuery.post("start-time")

repeater = (issue_post, initial_cb, next_cb) ->
    on_sched = null
    sched_next = () ->
        window.setTimeout(on_sched, 1000)
    on_sched = () ->
        issue_post().done(sched_next).done(next_cb)
    issue_post().done(sched_next).done(initial_cb)

window.setup_page_reloader = () ->
    timestamp = ""

    on_initial_time = (data) ->
        timestamp = data

    on_new_time = (data) ->
        if(data != timestamp)
            console.log("reloading!!")
            window.location.reload(true)

    repeater(issue_time_post, on_initial_time, on_new_time)
