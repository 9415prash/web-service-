<script setup>
import { computed, onMounted, ref } from "vue";
import { clearToken, getToken, request, saveToken } from "./api.js";

const posts = ref([]);
const currentUser = ref(null);
const loadingPosts = ref(true);
const authMode = ref("login");
const authLoading = ref(false);
const postLoading = ref(false);
const editingId = ref(null);
const errorMessage = ref("");
const notice = ref("");
const authForm = ref({ name: "", email: "", password: "" });
const postForm = ref({ title: "", content: "" });
const editForm = ref({ title: "", content: "" });

const isRegistering = computed(() => authMode.value === "register");

async function loadPosts() {
  loadingPosts.value = true;
  try {
    posts.value = await request("/api/posts");
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loadingPosts.value = false;
  }
}

async function restoreSession() {
  if (!getToken()) return;
  try {
    currentUser.value = await request("/api/me");
  } catch (error) {
    currentUser.value = null;
    if (error.status === 401) {
      clearToken();
      notice.value = "Your session expired. Please sign in again.";
    } else {
      errorMessage.value = error.message;
    }
  }
}

onMounted(() => {
  void Promise.all([loadPosts(), restoreSession()]);
});

function chooseAuthMode(mode) {
  authMode.value = mode;
  errorMessage.value = "";
  notice.value = "";
}

async function submitAuth() {
  errorMessage.value = "";
  notice.value = "";
  authLoading.value = true;
  try {
    if (isRegistering.value) {
      if (new TextEncoder().encode(authForm.value.password).length > 72) {
        throw new Error("Password must not exceed 72 UTF-8 bytes.");
      }
      await request("/api/auth/register", {
        method: "POST",
        body: {
          name: authForm.value.name,
          email: authForm.value.email,
          password: authForm.value.password,
        },
      });
      authMode.value = "login";
      authForm.value.password = "";
      notice.value = "Your account is ready. Sign in to start posting.";
      return;
    }

    const result = await request("/api/auth/login", {
      method: "POST",
      body: { email: authForm.value.email, password: authForm.value.password },
    });
    saveToken(result.accessToken);
    currentUser.value = await request("/api/me");
    authForm.value.password = "";
    notice.value = `Welcome back, ${currentUser.value.name}.`;
  } catch (error) {
    clearToken();
    currentUser.value = null;
    errorMessage.value = error.message;
  } finally {
    authLoading.value = false;
  }
}

function signOut() {
  clearToken();
  currentUser.value = null;
  editingId.value = null;
  postForm.value = { title: "", content: "" };
  notice.value = "You have signed out.";
  errorMessage.value = "";
}

async function createPost() {
  errorMessage.value = "";
  notice.value = "";
  postLoading.value = true;
  try {
    const created = await request("/api/posts", {
      method: "POST",
      body: postForm.value,
    });
    posts.value = [created, ...posts.value].slice(0, 50);
    postForm.value = { title: "", content: "" };
    notice.value = "Your post is now on the board.";
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    postLoading.value = false;
  }
}

function beginEdit(post) {
  editingId.value = post.id;
  editForm.value = { title: post.title, content: post.content };
  errorMessage.value = "";
  notice.value = "";
}

function cancelEdit() {
  editingId.value = null;
  editForm.value = { title: "", content: "" };
}

async function saveEdit(post) {
  errorMessage.value = "";
  notice.value = "";
  postLoading.value = true;
  try {
    const updated = await request(`/api/posts/${post.id}`, {
      method: "PUT",
      body: editForm.value,
    });
    posts.value = posts.value.map((item) => (item.id === updated.id ? updated : item));
    cancelEdit();
    notice.value = "Your changes have been saved.";
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    postLoading.value = false;
  }
}

async function deletePost(post) {
  if (!window.confirm("Delete this post? This cannot be undone.")) return;
  errorMessage.value = "";
  notice.value = "";
  postLoading.value = true;
  try {
    await request(`/api/posts/${post.id}`, { method: "DELETE" });
    posts.value = posts.value.filter((item) => item.id !== post.id);
    notice.value = "Your post has been deleted.";
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    postLoading.value = false;
  }
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
</script>

<template>
  <div class="page-shell">
    <header class="topbar">
      <a class="brand" href="/" aria-label="Campus Board home">
        <span class="brand-mark" aria-hidden="true">C</span>
        <span>Campus<span class="brand-light">Board</span></span>
      </a>
      <div class="topbar-note"><span class="status-dot"></span> Your campus, in the know</div>
      <div v-if="currentUser" class="account-chip">
        <span class="avatar" aria-hidden="true">{{ currentUser.name.charAt(0).toUpperCase() }}</span>
        <span class="account-name">{{ currentUser.name }}</span>
        <button class="text-button" type="button" @click="signOut">Sign out</button>
      </div>
      <a v-else class="topbar-link" href="#account">Join the community <span aria-hidden="true">↗</span></a>
    </header>

    <main>
      <section class="hero">
        <div class="hero-copy">
          <p class="eyebrow"><span class="eyebrow-line"></span> THE CAMPUS COMMUNITY BOARD</p>
          <h1>Good things<br />happen <em>between</em><br />classes.</h1>
          <p class="hero-description">
            Find your people, share a study session, or let campus know what's happening.
            A little more connected, one post at a time.
          </p>
          <a class="hero-cta" href="#board">Explore the board <span aria-hidden="true">↓</span></a>
        </div>
        <div class="hero-art" aria-hidden="true">
          <div class="sun"></div>
          <div class="orbit orbit-one"></div>
          <div class="orbit orbit-two"></div>
          <div class="art-note note-top"><span>MEET · MAKE · SHARE</span></div>
          <div class="art-note note-bottom">A PLACE FOR<br />WHAT'S NEXT<span>✳</span></div>
          <div class="art-spark spark-one">✳</div>
          <div class="art-spark spark-two">✦</div>
          <div class="art-caption">CAMPUS LIFE<br />LOOKS GOOD ON YOU</div>
        </div>
      </section>

      <div class="ticker" aria-label="Community guidelines">
        <div class="ticker-track">
          <span>SHARE A LITTLE GOOD</span><b>✳</b><span>FIND YOUR STUDY CREW</span><b>✳</b>
          <span>MAKE CAMPUS FEEL CLOSER</span><b>✳</b><span>SHARE A LITTLE GOOD</span><b>✳</b>
          <span>FIND YOUR STUDY CREW</span><b>✳</b><span>MAKE CAMPUS FEEL CLOSER</span><b>✳</b>
        </div>
      </div>

      <section class="community-section" id="board">
        <div class="section-heading">
          <div>
            <p class="eyebrow"><span class="eyebrow-line"></span> OUT THERE, RIGHT NOW</p>
            <h2>The community <em>board.</em></h2>
          </div>
          <div class="board-count"><span class="status-dot"></span> LATEST {{ posts.length }} POSTS</div>
        </div>

        <div v-if="errorMessage" class="message message-error" role="alert">
          <span>{{ errorMessage }}</span>
          <button class="message-close" type="button" aria-label="Dismiss error" @click="errorMessage = ''">×</button>
        </div>
        <div v-if="notice" class="message message-success" role="status">
          <span>{{ notice }}</span>
          <button class="message-close" type="button" aria-label="Dismiss message" @click="notice = ''">×</button>
        </div>

        <div class="board-layout">
          <aside id="account" class="side-column">
            <section v-if="currentUser" class="compose-card">
              <p class="eyebrow"><span class="eyebrow-line"></span> YOUR SPACE</p>
              <h3>Put it out<br /><em>there.</em></h3>
              <p class="card-intro">Have something good going on? Let the campus know.</p>
              <form class="stacked-form" @submit.prevent="createPost">
                <label for="post-title">A short headline</label>
                <input id="post-title" v-model="postForm.title" maxlength="160" required placeholder="Study session, anyone?" />
                <label for="post-content">What's the plan?</label>
                <textarea id="post-content" v-model="postForm.content" rows="5" required placeholder="Share the details with your campus community..."></textarea>
                <button class="button button-dark button-full" type="submit" :disabled="postLoading">
                  {{ postLoading ? "Posting..." : "Publish your post" }} <span aria-hidden="true">↗</span>
                </button>
              </form>
            </section>

            <section v-else class="auth-card">
              <p class="eyebrow"><span class="eyebrow-line"></span> BE PART OF IT</p>
              <h3 id="auth-title">{{ isRegistering ? "Find your people." : "Welcome back." }}</h3>
              <p class="card-intro">
                {{ isRegistering ? "Create an account to share what’s happening on campus." : "Sign in to post, edit, and keep your campus community close." }}
              </p>
              <div class="auth-tabs" role="tablist" aria-labelledby="auth-title">
                <button type="button" role="tab" :aria-selected="!isRegistering" :class="{ active: !isRegistering }" @click="chooseAuthMode('login')">Sign in</button>
                <button type="button" role="tab" :aria-selected="isRegistering" :class="{ active: isRegistering }" @click="chooseAuthMode('register')">Create account</button>
              </div>
              <form class="stacked-form" @submit.prevent="submitAuth">
                <template v-if="isRegistering">
                  <label for="auth-name">Your name</label>
                  <input id="auth-name" v-model="authForm.name" autocomplete="name" maxlength="100" required placeholder="How should we call you?" />
                </template>
                <label for="auth-email">Email address</label>
                <input id="auth-email" v-model="authForm.email" type="email" autocomplete="email" maxlength="254" required placeholder="you@campus.edu" />
                <label for="auth-password">Password</label>
                <input
                  id="auth-password"
                  v-model="authForm.password"
                  type="password"
                  :autocomplete="isRegistering ? 'new-password' : 'current-password'"
                  :minlength="isRegistering ? 12 : undefined"
                  required
                  placeholder="At least 12 characters"
                />
                <p v-if="isRegistering" class="form-hint">Use at least 12 characters. Your password is securely hashed.</p>
                <button class="button button-dark button-full" type="submit" :disabled="authLoading">
                  {{ authLoading ? "One moment..." : isRegistering ? "Create my account" : "Sign in to Campus Board" }}
                  <span aria-hidden="true">↗</span>
                </button>
              </form>
              <p class="privacy-note"><span aria-hidden="true">✳</span> Your posts are yours. Only you can change or remove them.</p>
            </section>

            <div class="guideline-card">
              <span class="guideline-icon" aria-hidden="true">✳</span>
              <p class="eyebrow">A GOOD PLACE STARTS WITH US</p>
              <p>Keep it kind, useful, and about campus life. Make someone feel welcome here.</p>
            </div>
          </aside>

          <div class="post-column" aria-live="polite">
            <div v-if="loadingPosts" class="empty-state">
              <span class="loading-mark" aria-hidden="true">✳</span>
              <h3>Finding the latest...</h3>
              <p>One moment while the community board loads.</p>
            </div>
            <div v-else-if="posts.length === 0" class="empty-state">
              <span class="empty-mark" aria-hidden="true">✳</span>
              <h3>It's a fresh start.</h3>
              <p>Nothing on the board just yet. Be the first to share something.</p>
            </div>
            <article v-for="(post, index) in posts" v-else :key="post.id" class="post-card">
              <div class="post-topline">
                <span class="post-category"><span class="post-category-dot"></span> COMMUNITY NOTE</span>
                <span class="post-number">NO. {{ String(posts.length - index).padStart(2, "0") }}</span>
              </div>
              <template v-if="editingId === post.id">
                <form class="stacked-form edit-form" @submit.prevent="saveEdit(post)">
                  <label :for="`edit-title-${post.id}`">Headline</label>
                  <input :id="`edit-title-${post.id}`" v-model="editForm.title" maxlength="160" required />
                  <label :for="`edit-content-${post.id}`">Details</label>
                  <textarea :id="`edit-content-${post.id}`" v-model="editForm.content" rows="5" required></textarea>
                  <div class="post-actions">
                    <button class="button button-dark" type="submit" :disabled="postLoading">Save changes</button>
                    <button class="button button-outline" type="button" @click="cancelEdit">Cancel</button>
                  </div>
                </form>
              </template>
              <template v-else>
                <h3 class="post-title">{{ post.title }}</h3>
                <p class="post-content">{{ post.content }}</p>
                <div class="post-footer">
                  <div class="author">
                    <span class="avatar avatar-small" aria-hidden="true">{{ post.ownerName.charAt(0).toUpperCase() }}</span>
                    <div>
                      <span class="author-name">{{ post.ownerName }}</span>
                      <time class="post-date" :datetime="post.createdAt">{{ formatDate(post.createdAt) }}</time>
                    </div>
                  </div>
                  <div v-if="currentUser && currentUser.id === post.ownerId" class="post-actions">
                    <button class="text-button" type="button" @click="beginEdit(post)">Edit</button>
                    <button class="text-button text-danger" type="button" :disabled="postLoading" @click="deletePost(post)">Delete</button>
                  </div>
                </div>
              </template>
            </article>
            <p v-if="!loadingPosts && posts.length === 50" class="board-footnote">
              Showing the latest 50 posts. That's the whole board, for now.
            </p>
          </div>
        </div>
      </section>
    </main>

    <footer class="footer">
      <a class="brand footer-brand" href="/" aria-label="Campus Board home">
        <span class="brand-mark" aria-hidden="true">C</span>
        <span>Campus<span class="brand-light">Board</span></span>
      </a>
      <p>A little more connected, one post at a time.</p>
      <a class="footer-top" href="#" aria-label="Back to top">BACK TO TOP ↑</a>
    </footer>
  </div>
</template>
