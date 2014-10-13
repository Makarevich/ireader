make_issue_auth_req = (post_data, andThen) -> () ->
    $.post 'auth', JSON.stringify(post_data), (data) ->
        if data.redirect_to?
            console.log("Redirecting to #{data.redirect_to}")
            window.location.href = data.redirect_to
        else if (data.result ? null) == 'OK'
            # window.access_token = data.access_token
            console.log("Access token verified")
            andThen?.resolve()
        else
            console.log "Unknown auth result", data

window.setup_auth = (reauth_node) ->
    auth_url = document.URL.match(/^(.*\/)[^\/]*$/)[1]
    # console.log("Auth url: #{auth_url}")
    andThen = window.jQuery.Deferred()
    make_issue_auth_req({redirect_to:auth_url}, andThen)()
    reauth_node.click make_issue_auth_req
        redirect_to:auth_url
        force:true
    andThen
