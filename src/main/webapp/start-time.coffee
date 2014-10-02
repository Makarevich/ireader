window.setup_page_reloader = () ->
    timestamp = ""
    issue_time_post = () -> window.jQuery.post("start-time")

    on_new_time = (data) ->
        if(data != timestamp) 
            console.log("reloading!!")
            window.location.reload(true)
    schedule_post = () ->
        window.setTimeout((() -> issue_time_post().done(on_new_time).always(schedule_post)), 1000)

    issue_time_post().done((data) ->
        timestamp = data
        schedule_post()
    )
