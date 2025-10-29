<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false; section>
    <#if section = "form">
        <div id="kc-form-wrapper">
            <div id="kc-form">
                <form action="${url.loginAction}" method="post" id="kc-contract-login-form">
                    <div class="form-group">
                        <label for="username" class="control-label">${msg("username")}</label>
                        <input type="text" id="username" name="username"
                               class="form-control" autofocus/>
                    </div>

                    <div class="form-group">
                        <label for="password" class="control-label">${msg("password")}</label>
                        <input type="password" id="password" name="password"
                               class="form-control"/>
                    </div>

                    <div id="kc-form-buttons" class="submit">
                        <input type="submit" class="btn btn-primary btn-block"
                               value="${msg("doLogIn")}"/>
                    </div>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
