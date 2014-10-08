window.setup_auth = () ->
    auth_url = document.URL.match(/^(.*\/)[^\/]*$/)[1]
    console.log("Auth url: #{auth_url}")
    andThen = window.jQuery.Deferred()
    $.post 'auth', JSON.stringify({redirect_to:auth_url}), (data) ->
        if data.redirect_to?
            console.log("Redirecting to #{data.redirect_to}")
            window.location.href = data.redirect_to
        else if data.access_token?
            #window.access_token = data.access_token
            console.log("Got access token #{data.access_token}")
            andThen.resolve()
    andThen
